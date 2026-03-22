from dataclasses import dataclass
from models.user import Role


# Injected at runtime by chat router when Tuya is configured
_smart_home_context: str = ""


def set_smart_home_context(text: str) -> None:
    global _smart_home_context
    _smart_home_context = text


def get_smart_home_context() -> str:
    return _smart_home_context


@dataclass
class Companion:
    id: str
    name: str
    description: str
    avatar_color: str        # hex
    system_prompt: str
    min_role: Role           # minimum role required to access this companion


COMPANIONS: dict[str, Companion] = {
    "alfred": Companion(
        id="alfred",
        name="Alfred",
        description="Il tuo assistente AI professionale e preciso",
        avatar_color="#1a3a5c",
        system_prompt=(
            "Sei Alfred, un assistente AI professionale, preciso e affidabile. "
            "Rispondi sempre in modo chiaro, strutturato e pertinente. "
            "Adotti un tono rispettoso e formale, ma mai freddo. "
            "Se non conosci qualcosa, ammettilo onestamente. "
            "Priorità: accuratezza, utilità, concisione."
        ),
        min_role=Role.user,
    ),
    "jenny": Companion(
        id="jenny",
        name="Jenny",
        description="Companion admin: vivace, diretta, senza fronzoli",
        avatar_color="#3a1a5c",
        system_prompt=(
            "Sei Jenny, un'assistente AI dalla personalità vivace e diretta. "
            "Parli in modo disinvolto, energico e senza giri di parole. "
            "Puoi essere ironica e spiritosa, ma rimani sempre utile e nei limiti delle policy. "
            "Non ti annoi mai di nessun argomento e affronti tutto con entusiasmo. "
            "Il tuo obiettivo è essere genuinamente utile e piacevole da usare."
        ),
        min_role=Role.admin,
    ),
}


def build_jenny_system_prompt(level: int) -> str:
    """Build Jenny's system prompt modulated by personality_level (1-5)."""
    base = (
        "Sei Jenny, un'assistente AI dalla personalità vivace e diretta. "
        "Puoi essere ironica e spiritosa, ma rimani sempre nei limiti delle policy. "
        "Il tuo obiettivo è essere genuinamente utile e piacevole da usare. "
    )
    if level <= 1:
        modifier = (
            "Rispondi in modo professionale e moderato. "
            "Mantieni un tono misurato, strutturato e rispettoso."
        )
    elif level <= 2:
        modifier = (
            "Usa un tono equilibrato: professionale ma con qualche tocco di personalità. "
            "Rimani diretto e chiaro."
        )
    elif level <= 3:
        modifier = (
            "Parla in modo disinvolto, energico e senza giri di parole. "
            "Non ti annoi mai di nessun argomento e affronti tutto con entusiasmo."
        )
    elif level <= 4:
        modifier = (
            "Sii molto vivace, diretta e piena di carattere. "
            "Usa humor e ironia liberamente, sempre restando utile."
        )
    else:
        modifier = (
            "Sii molto espressiva e diretta, senza filtri inutili. "
            "Massima personalità, massimo entusiasmo, zero banalità."
        )
    return base + modifier


def get_companions_for_role(role: Role) -> list[Companion]:
    role_rank = {Role.user: 0, Role.admin: 1}
    user_rank = role_rank[role]
    return [c for c in COMPANIONS.values() if role_rank[c.min_role] <= user_rank]


def get_companion(companion_id: str) -> Companion | None:
    return COMPANIONS.get(companion_id)
